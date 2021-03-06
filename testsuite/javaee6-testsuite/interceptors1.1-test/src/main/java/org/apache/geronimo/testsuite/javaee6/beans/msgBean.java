/**
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.apache.geronimo.testsuite.javaee6.beans;

import javax.ejb.Singleton;

@Singleton
public class msgBean {
    
    private String msg = new String();
        public String getOutput(){
        try{
            Thread.sleep(3000);//3 seconds
            }catch(Exception e)
            {
            }
       return msg;
    }

    public void setOutput(String s){
        this.msg = s;
    }
 
}
