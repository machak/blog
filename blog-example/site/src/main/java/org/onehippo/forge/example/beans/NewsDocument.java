
package org.onehippo.forge.example.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="blogexample:newsdocument")
public class NewsDocument extends BaseDocument{

    public String getTitle() {
        return getProperty("blogexample:title");
    }
    
    public String getSummary() {
        return getProperty("blogexample:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("blogexample:body");    
    }
    
    public Calendar getDate() {
        return getProperty("blogexample:date");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("blogexample:image", HippoGalleryImageSetBean.class);
    }
    
}
