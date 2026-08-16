package com.isspredictor.iss_predictor_service.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

//  Entry point that makes surefire discover and run the Cucumber feature files.
//  Named "CucumberTest" (not "CucumberSuite" or similar) specifically because
//  our pom.xml's surefire config only includes **/*Test.java - this class
//  wouldn't be picked up at all under a different naming pattern.

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.isspredictor.bdd")
public class CucumberTest {
}