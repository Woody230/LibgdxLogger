/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright 2013 Jake Wharton
 * Modifications Copyright 2018 Brandon Selzer
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
 *
 * Log: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/util/Log.java
 * Timber: https://github.com/JakeWharton/timber
 */

package com.outlook.bselzer3.libgdxlogger;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibgdxLogger
{
    private enum Priority
    {
        LOG,
        DEBUG,
        ERROR
    }

    private static final int MAX_LOG_LENGTH = 4000;
    private static final int MAX_TAG_LENGTH = 23;
    private static final int CALL_STACK_INDEX_PRIVATE = 3; //0: getTag(), 1: print(), 2: log/debug/error(), 3: caller method
    private static final int CALL_STACK_INDEX_PUBLIC = 2; //0: getTag(), 1: getTag(), 2: caller method
    private static final Pattern ANONYMOUS_CLASS_PATTERN = Pattern.compile("(\\$\\d+)+$");
    private static Application app = Gdx.app;

    private LibgdxLogger()
    {

    }

    public static void log(String message)
    {
        print(Priority.LOG, message, null);
    }

    public static void log(String message, Throwable exception)
    {
        print(Priority.LOG, message, exception);
    }

    public static void log(Throwable exception)
    {
        print(Priority.LOG, null, exception);
    }

    public static void debug(String message)
    {
        print(Priority.DEBUG, message, null);
    }

    public static void debug(String message, Throwable exception)
    {
        print(Priority.DEBUG, message, exception);
    }

    public static void debug(Throwable exception)
    {
        print(Priority.DEBUG, null, exception);
    }

    public static void error(String message)
    {
        print(Priority.ERROR, message, null);
    }

    public static void error(String message, Throwable exception)
    {
        print(Priority.ERROR, message, exception);
    }

    public static void error(Throwable exception)
    {
        print(Priority.ERROR, null, exception);
    }

    @SuppressWarnings("ConstantConditions")
    private static void print(Priority priority, String message, Throwable exception)
    {
        String tag = getTag(CALL_STACK_INDEX_PRIVATE);

        if(message == null)
        {
            message = "";
        }

        if(exception != null)
        {
            message += "\n" + getStackTraceString(exception);
        }

        int length = message.length();
        if(app.getType() != Application.ApplicationType.Android || length < MAX_LOG_LENGTH)
        {
            useLogger(priority, tag, message);
            return;
        }

        for(int i = 0; i < length; i++)
        {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;

            do
            {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                useLogger(priority, tag, message.substring(i, end));
                i = end;
            } while(i < newline);
        }
    }

    private static void useLogger(Priority priority, String tag, String message)
    {
        switch(priority)
        {
            case ERROR:
                app.error(tag, message);
                break;
            case DEBUG:
                app.debug(tag, message);
                break;
            case LOG:
                app.log(tag, message);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported priority.");
        }
    }

    public static String getStackTraceString(Throwable exception)
    {
        if(exception == null)
        {
            return "";
        }

        //This is to reduce the amount of log spew that apps do in the non-error condition of the network being unavailable.
        Throwable t = exception;
        while(t != null)
        {
            if(t instanceof UnknownHostException)
            {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static String getTag(int callStackIndex)
    {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if(stackTrace.length <= callStackIndex)
        {
            throw new IllegalStateException("Synthetic stacktrace did not have enough elements: are you using proguard?");
        }

        StackTraceElement element = stackTrace[callStackIndex];

        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS_PATTERN.matcher(tag);
        if(m.find())
        {
            tag = m.replaceAll("");
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1);

        //Tag length limit was removed in API 24. Unable to do the check since that is platform specific.
        if(app.getType() != Application.ApplicationType.Android || tag.length() <= MAX_TAG_LENGTH) //|| Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            return tag;
        }

        return tag.substring(0, MAX_TAG_LENGTH);
    }

    public static String getTag()
    {
        return getTag(CALL_STACK_INDEX_PUBLIC);
    }
}