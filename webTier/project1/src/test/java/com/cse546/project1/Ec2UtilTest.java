package com.cse546.project1;

import org.junit.jupiter.api.Test;

public class Ec2UtilTest {

    @Test
    void instanceCount() {
        Ec2Util ec2Util = new Ec2Util();
        System.out.println(ec2Util.getNumOfActiveInstances());
    }
}
