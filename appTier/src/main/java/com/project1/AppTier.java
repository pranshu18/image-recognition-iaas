package com.project1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.EC2MetadataUtils;

public class AppTier {

	private static final int MAX_MESSAGES = 1;
	private static final int MAX_WAIT_TIME = 15;

	private AmazonSQS amznSqs;
	private AmazonS3 amznS3;
	private AmazonEC2 amznEc2;
	private String sqsInputUrl="https://sqs.us-east-1.amazonaws.com/359422161956/input-queue";
	private String sqsOutputUrl="https://sqs.us-east-1.amazonaws.com/359422161956/output-queue";
	private String inputBucketName="input-images-f22";
	private String outputBucketName="output-labels-f22";


	public AppTier(AmazonSQS amznSqs, AmazonS3 amznS3, AmazonEC2 amznEc2) {
		
		this.amznSqs = amznSqs;
		this.amznS3 = amznS3;
		this.amznEc2 = amznEc2;

	}

	public void performAppTierTasks() {
		while(true) {
			try {
				ReceiveMessageRequest rmrq = new ReceiveMessageRequest(sqsInputUrl).withMessageAttributeNames("fileName").withAttributeNames("");

				rmrq.setMaxNumberOfMessages(MAX_MESSAGES);
				rmrq.setWaitTimeSeconds(MAX_WAIT_TIME);

				List<Message> messages = amznSqs.receiveMessage(rmrq).getMessages();

				Message msg;
				if(messages!=null && !messages.isEmpty()) {
					msg=messages.get(0);
				}else {
					msg=null;
					shutdown();
					break;
				}

				Map<String,MessageAttributeValue> rcvdMessageAttributes = msg.getMessageAttributes();
				String imageNameKey = rcvdMessageAttributes.get("fileName").getStringValue();
				String encodedImage = msg.getBody();
				
				byte[] decodedImg = Base64.getDecoder().decode(encodedImage.getBytes(StandardCharsets.UTF_8));

				InputStream imageInputStream = new ByteArrayInputStream(decodedImg);
				
				Path imgPath = Paths.get(imageNameKey);
				Files.copy(imageInputStream, imgPath, StandardCopyOption.REPLACE_EXISTING);

				amznS3.putObject(inputBucketName, imageNameKey, imgPath.toFile());

				String command="python3 image_classification.py ../" + imgPath;
				Runtime run = Runtime.getRuntime();
				Process process = run.exec(command,null,new File("/home/ubuntu/classifier"));
				process.waitFor();
				String res = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
				process.destroy();
				
				System.out.println(res);
				amznS3.putObject(outputBucketName, imageNameKey, res);

				SendMessageRequest sendMsgRequest = new SendMessageRequest()
						.withQueueUrl(sqsOutputUrl)
						.withMessageBody(res);
				
				Map<String,MessageAttributeValue> messageAttributes = new HashMap<String,MessageAttributeValue>();
				MessageAttributeValue msgAttrVal = new MessageAttributeValue();
				msgAttrVal.setDataType("String");
				msgAttrVal.setStringValue(imageNameKey);
				messageAttributes.put("fileName", msgAttrVal);
				
				sendMsgRequest.setMessageAttributes(messageAttributes);
				amznSqs.sendMessage(sendMsgRequest);

				Files.deleteIfExists(imgPath);
				amznSqs.deleteMessage(sqsInputUrl, msg.getReceiptHandle());

			}catch(Exception e) {

				e.printStackTrace();
				shutdown();
				break;
			}
		}

	}

	private void shutdown() {

		String instanceId = EC2MetadataUtils.getInstanceId();
		System.out.println("Instance ID - "+instanceId);
		TerminateInstancesRequest ti = new TerminateInstancesRequest().withInstanceIds(instanceId);

		amznEc2.terminateInstances(ti);

	}

}
