/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */

package com.datastax.driver.jdbc;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.jdbc.Utils;

public class UtilsUnitTest
{
    private static final Logger LOG = LoggerFactory.getLogger(CollectionsUnitTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {}

    @Test
    public void testParseURL() throws Exception
    {
        String happypath = "jdbc:cassandra://localhost:9042/Keyspace1?version=3.0.0&consistency=QUORUM";
        Properties props = Utils.parseURL(happypath);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        AssertJUnit.assertEquals("Keyspace1", props.getProperty(Utils.TAG_DATABASE_NAME));
        AssertJUnit.assertEquals("3.0.0", props.getProperty(Utils.TAG_CQL_VERSION));
        AssertJUnit.assertEquals("QUORUM", props.getProperty(Utils.TAG_CONSISTENCY_LEVEL));
                       
        String consistencyonly = "jdbc:cassandra://localhost/Keyspace1?consistency=QUORUM";
        props = Utils.parseURL(consistencyonly);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        AssertJUnit.assertEquals("Keyspace1", props.getProperty(Utils.TAG_DATABASE_NAME));
        AssertJUnit.assertEquals("QUORUM", props.getProperty(Utils.TAG_CONSISTENCY_LEVEL));
        assert(props.getProperty(Utils.TAG_CQL_VERSION)==null);
       
        String noport = "jdbc:cassandra://localhost/Keyspace1?version=2.0.0";
        props = Utils.parseURL(noport);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        AssertJUnit.assertEquals("Keyspace1", props.getProperty(Utils.TAG_DATABASE_NAME));
        AssertJUnit.assertEquals("2.0.0", props.getProperty(Utils.TAG_CQL_VERSION));
        
        String noversion = "jdbc:cassandra://localhost:9042/Keyspace1";
        props = Utils.parseURL(noversion);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        AssertJUnit.assertEquals("Keyspace1", props.getProperty(Utils.TAG_DATABASE_NAME));
        assert(props.getProperty(Utils.TAG_CQL_VERSION)==null);
        
        String nokeyspaceonly = "jdbc:cassandra://localhost:9042?version=2.0.0";
        props = Utils.parseURL(nokeyspaceonly);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        assert(props.getProperty(Utils.TAG_DATABASE_NAME)==null);
        AssertJUnit.assertEquals("2.0.0", props.getProperty(Utils.TAG_CQL_VERSION));
        
        String nokeyspaceorver = "jdbc:cassandra://localhost:9042";
        props = Utils.parseURL(nokeyspaceorver);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        assert(props.getProperty(Utils.TAG_DATABASE_NAME)==null);
        assert(props.getProperty(Utils.TAG_CQL_VERSION)==null);
        
        String withloadbalancingpolicy = "jdbc:cassandra://localhost:9042?loadbalancing=TokenAwarePolicy-DCAwareRoundRobinPolicy&primarydc=DC1";
        props = Utils.parseURL(withloadbalancingpolicy);
        AssertJUnit.assertEquals("localhost", props.getProperty(Utils.TAG_SERVER_NAME));
        AssertJUnit.assertEquals("9042", props.getProperty(Utils.TAG_PORT_NUMBER));
        assert(props.getProperty(Utils.TAG_DATABASE_NAME)==null);
        assert(props.getProperty(Utils.TAG_CQL_VERSION)==null);
        AssertJUnit.assertEquals("TokenAwarePolicy-DCAwareRoundRobinPolicy", props.getProperty(Utils.TAG_LOADBALANCING_POLICY));
        AssertJUnit.assertEquals("DC1", props.getProperty(Utils.TAG_PRIMARY_DC));
    }
    
    @Test
    public void testLoadBalancingPolicyParsing() throws Exception
    {
    	String lbPolicyStr = "RoundRobinPolicy()";
    	System.out.println(lbPolicyStr);
    	AssertJUnit.assertTrue(Utils.parsePolicy(lbPolicyStr) instanceof RoundRobinPolicy);
    	System.out.println("====================");
    	lbPolicyStr = "TokenAwarePolicy(RoundRobinPolicy())";
    	System.out.println(lbPolicyStr);
    	AssertJUnit.assertTrue(Utils.parsePolicy(lbPolicyStr) instanceof TokenAwarePolicy);
    	System.out.println("====================");
    	lbPolicyStr = "DCAwareRoundRobinPolicy(\"dc1\")";
    	System.out.println(lbPolicyStr);
    	AssertJUnit.assertTrue(Utils.parsePolicy(lbPolicyStr) instanceof DCAwareRoundRobinPolicy);
    	System.out.println("====================");
    	lbPolicyStr = "TokenAwarePolicy(DCAwareRoundRobinPolicy(\"dc1\"))";
    	System.out.println(lbPolicyStr);
    	AssertJUnit.assertTrue(Utils.parsePolicy(lbPolicyStr) instanceof TokenAwarePolicy);    	
    	System.out.println("====================");
    	lbPolicyStr = "TokenAwarePolicy";
    	System.out.println(lbPolicyStr);
    	AssertJUnit.assertTrue(Utils.parsePolicy(lbPolicyStr)==null);
    	System.out.println("====================");
    	
    }
  
    @Test
    public void testCreateSubName() throws Exception
    {
        String happypath = "jdbc:cassandra://localhost:9042/Keyspace1?consistency=QUORUM&version=3.0.0";
        Properties props = Utils.parseURL(happypath);
        
        if (LOG.isDebugEnabled()) LOG.debug("happypath    = '{}'", happypath);

        
        String result = Utils.createSubName(props);
        if (LOG.isDebugEnabled()) LOG.debug("result       = '{}'", Utils.PROTOCOL+result);
        
        AssertJUnit.assertEquals(happypath, Utils.PROTOCOL+result);
    }
}
