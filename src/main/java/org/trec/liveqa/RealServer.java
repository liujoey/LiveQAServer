package org.trec.liveqa;

import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by joey on 4/29/16.
 */
public class RealServer extends TrecLiveQaDemoServer {

    private class Question {
        public String qid;
        public String title;
        public String body;
        public String category;
        public String answer;
        public String resources;
        public String focus;

        public Question(String qid, String title, String body, String category) {
            this.qid = qid;
            this.title = title;
            this.body = body;
            this.category = category;
            answer = null;
            resources = null;
            focus = null;
        }

        @Override
        public String toString() {
            return qid + "\t" + title + "\t" + body + "\t" + category;
        }
    }

    private final static String relayServerURI = "ws://localhost:8080/";
    private Map<String, Question> questions = new HashMap<>();

    WebSocketClient cc;
    Logger logger = Logger.getLogger(RealServer.class);

    public synchronized void addQuestion(Question question) {
        questions.put(question.qid, question);
    }

    public synchronized void removeQuestion(String qid) {
        questions.remove(qid);
    }

    public synchronized void setAnswer(String qid, String answer, String resources, String focus) {
        if (questions.containsKey(qid)) {
            Question question = questions.get(qid);
            question.answer = answer;
            question.resources = resources;
            question.focus = focus;
        }
    }

    public String getAnswer(String qid) {
        if (questions.containsKey(qid)) {
            return questions.get(qid).answer;
        }
        return null;
    }

    public String getResources(String qid) {
        if (questions.containsKey(qid)) {
            return questions.get(qid).resources;
        }
        return null;
    }

    public String getFocus(String qid) {
        if (questions.containsKey(qid)) {
            return questions.get(qid).focus;
        }
        return null;
    }

    public RealServer(int port) {
        super(port);
        try {
            cc = new WebSocketClient(new URI(relayServerURI), new Draft_17()) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("Connected to relay server.");
                    registerClient();
                }

                @Override
                public void onMessage(String message) {
                    if (message.equals("Client registered.")) { // register confirmation from server
                        logger.info(message);
                    } else { // answer message
                        try {
                            logger.info(message);
                            String[] answer = message.split("\t");
                            assert answer.length == 4 : message + " is not a correct answer format!";
                            String qid = answer[0];
                            String answer_text = answer[1];
                            String resources = answer[2];
                            String focus = answer[3];
                            setAnswer(qid, answer_text, resources, focus);
                            logger.info("Answer returned: " + message);
                        } catch (IndexOutOfBoundsException e) {
                            logger.error("Unexpected answer format!");
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("Disconnected from relay server.");
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("Exception occured ...\n" + ex + "\n");
                    ex.printStackTrace();
                }
            };
            cc.connect();
        } catch (URISyntaxException e) {
            logger.error(relayServerURI + " is not a valid WebSocket URI\n");
        }
    }

    private void registerClient() {
        cc.send("REGISTER: LiveQA");
    }

    @Override
    protected AnswerAndResourcesAndSummaries getAnswerAndResourcesAndSummaries(String qid, String title, String body, String category) throws InterruptedException {
        Question question = new Question(qid, title, body, category);
        addQuestion(question);
        cc.send(question.toString());
        int count = 0;
        while (getAnswer(qid) == null && count < 59) {
            count ++;
            Thread.sleep(1000);
        }
        String answer = getAnswer(qid);
        String resources = getResources(qid);
        String focus = getFocus(qid);
        removeQuestion(qid);
        if (answer == null) {
            EXCUSE = "ICON engine timeout!";
            return null;
        }
        return new AnswerAndResourcesAndSummaries(answer, resources, focus, "", "");
    }

    public static void main(String[] args) throws IOException {
        RealServer server =
            new RealServer(args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]));
        server.start();
        System.in.read();
        server.stop();
    }
}
