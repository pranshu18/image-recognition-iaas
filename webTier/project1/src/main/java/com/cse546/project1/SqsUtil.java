package com.cse546.project1;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.*;


public class SqsUtil {

    private static final String ACCESS_KEY = "xxx";
    private static final String SECRET_KEY = "yyy";
    private static Map<String, String> resultCache = new HashMap<>();
    static final String requestQueueUrl = "https://sqs.us-east-1.amazonaws.com/359422161956/input-queue";
    static final String responseQueueUrl = "https://sqs.us-east-1.amazonaws.com/359422161956/output-queue";
    private final AWSStaticCredentialsProvider credentials;

    public SqsUtil(){
        credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                ACCESS_KEY,
                SECRET_KEY
        ));
    }
    protected void sendImage(String fileName, byte[] imageData) {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").withCredentials(credentials).build();
        final SendMessageRequest send_msg_request;
        send_msg_request = new SendMessageRequest()
                .withQueueUrl(requestQueueUrl)
                .withMessageBody(Base64.getEncoder().encodeToString(imageData))
                .addMessageAttributesEntry("fileName", new MessageAttributeValue().withDataType("String")
                        .withStringValue(fileName))
                .withDelaySeconds(1);

        sqs.sendMessage(send_msg_request);
    }

    protected String readResult(String fileName) {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").withCredentials(credentials).build();
        final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(responseQueueUrl)
                .withWaitTimeSeconds(2);
        boolean resultFound = false;
        String result = null;

        while(!resultFound){
            if(resultCache.containsKey(fileName)){
                result = resultCache.get(fileName);
                resultCache.remove(fileName);
                resultFound = true;
            }else{
                List<Message> messages = sqs.receiveMessage(receiveMessageRequest.withMessageAttributeNames("All")).getMessages();
                if(messages.size() < 1){
                    continue;
                }
                Message m = messages.get(0);
                if(fileName.equals(m.getMessageAttributes().get("fileName").getStringValue())){
                    result = m.getBody().split(",")[1];
                    resultFound = true;
                } else {
                    resultCache.put(m.getMessageAttributes().get("fileName").getStringValue(), m.getBody().split(",")[1]);
                }
                sqs.deleteMessage(responseQueueUrl, m.getReceiptHandle());
            }
        }
        return result;
    }

    protected static int getInputQueueSize() {
        AWSStaticCredentialsProvider credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                ACCESS_KEY,
                SECRET_KEY
        ));
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").withCredentials(credentials).build();
        GetQueueAttributesResult attributes = sqs.getQueueAttributes(requestQueueUrl, Collections.singletonList("ApproximateNumberOfMessages"));
        String size = attributes.getAttributes().get("ApproximateNumberOfMessages");

        return Integer.parseInt(size);
    }
}