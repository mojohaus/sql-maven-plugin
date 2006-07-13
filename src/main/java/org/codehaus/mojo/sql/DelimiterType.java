package org.codehaus.mojo.sql;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

/**
 * Created by IntelliJ IDEA.
 * User: topping
 * Date: Jan 27, 2006
 * Time: 8:33:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelimiterType
{
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
     * @return an array holding all possible values of the enumeration.
     * The order of elements must be fixed so that <tt>indexOfValue(String)</tt>
     * always return the same index for the same value.
     */
    public String[] getValues()
    {
        return new String[] { NORMAL, ROW };
    }

    /** bean constructor */
    protected DelimiterType()
    {
    }

    /**
     * Invoked by {@link org.apache.tools.ant.IntrospectionHelper IntrospectionHelper}.
     */
    public final void setValue( String value )
    {
        int index = indexOfValue( value );
        if ( index == -1 )
        {
            throw new RuntimeException( value + " is not a legal value for this attribute" );
        }
        this.index = index;
        this.value = value;
    }

    /**
     * Is this value included in the enumeration?
     */
    public final boolean containsValue( String value )
    {
        return ( indexOfValue( value ) != -1 );
    }

    /**
     * get the index of a value in this enumeration.
     * @param value the string value to look for.
     * @return the index of the value in the array of strings
     * or -1 if it cannot be found.
     * @see #getValues()
     */
    public final int indexOfValue( String value )
    {
        String[] values = getValues();
        if ( values == null || value == null )
        {
            return -1;
        }
        for ( int i = 0; i < values.length; i++ )
        {
            if ( value.equals( values[i] ) )
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the selected value.
     */
    public final String getValue()
    {
        return value;
    }

    /**
     * @return the index of the selected value in the array.
     * @see #getValues()
     */
    public final int getIndex()
    {
        return index;
    }

    /**
     * Convert the value to its string form.
     *
     * @return the string form of the value.
     */
    public String toString()
    {
        return getValue();
    }

}
