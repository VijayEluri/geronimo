/**
 *
 *  Copyright 2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.interop.generator;

public class JCatchStatement extends JBlockStatement {
    protected JVariable _var;

    public JCatchStatement(JVariable v) {
        super();
        _var = v;
    }

    public JVariable getVariable() {
        return _var;
    }

    public int hashCode() {
        return _var.hashCode();
    }

    public boolean equals(Object other) {
        boolean rc = false;

        if (this == other) {
            rc = true;
        } else if (other instanceof JCatchStatement) {
            JCatchStatement cs = (JCatchStatement) other;

            if (cs._var.getType().equals(_var.getType())) {
                rc = true;
            }
        } else if (other instanceof JVariable) {
            JVariable v = (JVariable) other;

            if (v.getType().equals(_var.getType())) {
                rc = true;
            }
        }


        return rc;
    }
}

/*
public class JCatchStatement extends JStatement
{
    protected JVariable          _var;
    protected JBlockStatement    _statements;

    public JCatchStatement( JVariable v )
    {
        _var = v;
        _statements = new JBlockStatement();
    }

    public void addStatement( JStatement s )
    {
        _statements.addStatement( s );
    }

    public JBlockStatement getStatement()
    {
        return _statements;
    }

    public JVariable getVariable()
    {
        return _var;
    }

    public int hashCode()
    {
        return _var.hashCode();
    }

    public boolean equals( Object other )
    {
        boolean rc = false;

        if (this == other)
        {
            rc = true;
        }
        else if (other instanceof JCatchStatement)
        {
            JCatchStatement cs = (JCatchStatement)other;

            if (cs._var.getType().equals(_var.getType()))
            {
                rc = true;
            }
        }
        else if (other instanceof JVariable)
        {
            JVariable v = (JVariable)other;

            if (v.getType().equals( _var.getType() ))
            {
                rc = true;
            }
        }


        return rc;
    }
}

*/