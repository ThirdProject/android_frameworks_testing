/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.uiautomator;

import android.os.Bundle;
import android.os.Process;

import com.android.uiautomator.testrunner.UiAutomatorTestRunner;

import java.util.ArrayList;
import java.util.List;

public class UiAutomator {

    private static final String CLASS_PARAM = "class";
    private static final String DEBUG_PARAM = "debug";
    private static final String RUNNER_PARAM = "runner";
    private static final String CLASS_SEPARATOR = ",";
    private static final int ARG_OK = 0;
    private static final int ARG_FAIL_INCOMPLETE_E = -1;
    private static final int ARG_FAIL_INCOMPLETE_C = -2;
    private static final int ARG_FAIL_NO_CLASS = -3;
    private static final int ARG_FAIL_RUNNER = -4;
    private static final int ARG_FAIL_UNSUPPORTED = -99;

    private Bundle mParams = new Bundle();
    private List<String> mTestClasses = new ArrayList<String>();
    private boolean mDebug;
    private String mRunner;

    public static void main(String[] args) {
        // set the name as it appears in ps/top etc
        Process.setArgV0("uiautomator");
        new UiAutomator().run(args);
    }

    private void run(String[] args) {
        int ret = parseArgs(args);
        switch (ret) {
            case ARG_FAIL_INCOMPLETE_C:
                System.err.println("Incomplete '-c' parameter.");
                System.exit(ARG_FAIL_INCOMPLETE_C);
                break;
            case ARG_FAIL_INCOMPLETE_E:
                System.err.println("Incomplete '-e' parameter.");
                System.exit(ARG_FAIL_INCOMPLETE_E);
                break;
            case ARG_FAIL_UNSUPPORTED:
                System.err.println("Unsupported standaline parameter.");
                System.exit(ARG_FAIL_UNSUPPORTED);
                break;
            default:
                break;
        }
        if (mTestClasses.isEmpty()) {
            System.err.println("Please specify at least one test class to run.");
            System.exit(ARG_FAIL_NO_CLASS);
        }
        getRunner().run(mTestClasses, mParams, mDebug);
    }

    private int parseArgs(String[] args) {
        // we are parsing for these parameters:
        // -e <key> <value>
        // key-value pairs
        // special ones are:
        // key is "class", parameter is passed onto JUnit as class name to run
        // key is "debug", parameter will determine whether to wait for debugger
        // to attach
        // -c <class name>
        // equivalent to -e class <class name>, i.e. passed onto JUnit
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-e")) {
                if (i + 2 < args.length) {
                    String key = args[++i];
                    String value = args[++i];
                    if (CLASS_PARAM.equals(key)) {
                        addTestClasses(value);
                    } else if (DEBUG_PARAM.equals(key)) {
                        mDebug = "true".equals(value) || "1".equals(value);
                    } else if (RUNNER_PARAM.equals(key)) {
                        mRunner = value;
                    } else {
                        mParams.putString(key, value);
                    }
                } else {
                    return ARG_FAIL_INCOMPLETE_E;
                }
            } else if (args[i].equals("-c")) {
                if (i + 1 < args.length) {
                    addTestClasses(args[++i]);
                } else {
                    return ARG_FAIL_INCOMPLETE_C;
                }
            } else {
                return ARG_FAIL_UNSUPPORTED;
            }
        }
        return ARG_OK;
    }

    protected UiAutomatorTestRunner getRunner() {
        if (mRunner == null) {
            return new UiAutomatorTestRunner();
        }
        // use reflection to get the runner
        Object o = null;
        try {
            Class<?> clazz = Class.forName(mRunner);
            o = clazz.newInstance();
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Cannot find runner: " + mRunner);
            System.exit(ARG_FAIL_RUNNER);
        } catch (InstantiationException ie) {
            System.err.println("Cannot instantiate runner: " + mRunner);
            System.exit(ARG_FAIL_RUNNER);
        } catch (IllegalAccessException iae) {
            System.err.println("Constructor of runner " + mRunner + " is not accessibile");
            System.exit(ARG_FAIL_RUNNER);
        }
        try {
            UiAutomatorTestRunner runner = (UiAutomatorTestRunner)o;
            return runner;
        } catch (ClassCastException cce) {
            System.err.println("Specified runner is not subclass of "
                    + UiAutomatorTestRunner.class.getSimpleName());
            System.exit(ARG_FAIL_RUNNER);
        }
        // won't reach here
        return null;
    }

    /**
     * Add test classes from a potentially comma separated list
     * @param classes
     */
    private void addTestClasses(String classes) {
        String[] classArray = classes.split(CLASS_SEPARATOR);
        for (String clazz : classArray) {
            mTestClasses.add(clazz);
        }
    }
}