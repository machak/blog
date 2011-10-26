
package org.onehippo.forge.example.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="blogexample:textdocument")
public class TextDocument extends BaseDocument{


    public String getTitle() {
        return getProperty("blogexample:title");
    }
    
    public String getSummary() {
        return getProperty("blogexample:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("blogexample:body");    
    }
}
