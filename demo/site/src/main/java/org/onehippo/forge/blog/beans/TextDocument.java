
package org.onehippo.forge.blog.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="blog demo:textdocument")
public class TextDocument extends BaseDocument{


    public String getTitle() {
        return getProperty("blog demo:title");
    }
    
    public String getSummary() {
        return getProperty("blog demo:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("blog demo:body");    
    }
}
