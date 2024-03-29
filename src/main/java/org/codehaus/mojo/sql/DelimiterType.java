package org.codehaus.mojo.sql;

/*
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
 */

/**
 * @author Brian Topping
 */
public class DelimiterType {
    public static final String NORMAL = "normal";

    public static final String ROW = "row";

    /**
     * The selected value in this enumeration.
     */
    protected String value;

    /**
     * the index of the selected value in the array.
     */
    private int index = -1;

    /**
     * This is the only method a subclass needs to implement.
     *
     * @return an array holding all possible values of the enumeration. The order of elements must be fixed so that
     *         <code>indexOfValue(String)</code> always return the same index for the same value.
     */
    public String[] getValues() {
        return new String[] {NORMAL, ROW};
    }

    /** bean constructor */
    protected DelimiterType() {}

    /**
     * Set the delimiterValue. Use DelimiterType.NORMAL or DelimiterType.ROW
     *
     * @param value
     */
    public final void setValue(String value) {
        int index = indexOfValue(value);
        if (index == -1) {
            throw new IllegalArgumentException(value + " is not a legal value for this attribute");
        }
        this.index = index;
        this.value = value;
    }

    /**
     * Is this value included in the enumeration?
     *
     * @param value
     * @return true if this value is supported
     */
    public final boolean containsValue(String value) {
        return (indexOfValue(value) != -1);
    }

    /**
     * get the index of a value in this enumeration.
     *
     * @param value the string value to look for.
     * @return the index of the value in the array of strings or -1 if it cannot be found.
     * @see #getValues()
     */
    public final int indexOfValue(String value) {
        String[] values = getValues();
        if (values == null || value == null) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the selected value.
     */
    public final String getValue() {
        return value;
    }

    /**
     * @return the index of the selected value in the array.
     * @see #getValues()
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Convert the value to its string form.
     *
     * @return the string form of the value.
     */
    public String toString() {
        return getValue();
    }
}
