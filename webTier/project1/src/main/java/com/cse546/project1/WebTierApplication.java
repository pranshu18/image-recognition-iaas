package com.cse546.project1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebTierApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebTierApplication.class, args);
		Ec2Util ec2 = new Ec2Util();
		ec2.autoscaleAppTier();
	}

}
