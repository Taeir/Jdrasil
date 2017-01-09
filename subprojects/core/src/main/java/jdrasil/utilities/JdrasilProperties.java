/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.utilities;

import java.util.Properties;

/**
 * Static overlay of the properties class. This is globally used to configurate Jdrasil.
 * If Jdrasil is used as standalone, this is done within the main method by the arguments provided to Jdrasil.
 * If Jdrasil is used as library, properties have to be set manually.
 * @author Max Bannach
 */
public class JdrasilProperties {

    /** Global properties object used by Jdrasil. */
    private static Properties properties;

    /** Static constructor for the properties object. */
    static {
        properties = new Properties();
    }

    /**
     * Checks if the property for 'key' is set.
     * @param key
     * @return true if there is an entry for the key
     */
    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Get the entry stored for the given key.
     * @param key
     * @return the entry
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Sets the property 'key' to the given 'value'.
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

}