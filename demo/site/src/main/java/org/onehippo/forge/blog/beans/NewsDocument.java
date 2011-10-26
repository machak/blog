
package org.onehippo.forge.blog.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="blog demo:newsdocument")
public class NewsDocument extends BaseDocument{

    public String getTitle() {
        return getProperty("blog demo:title");
    }
    
    public String getSummary() {
        return getProperty("blog demo:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("blog demo:body");    
    }
    
    public Calendar getDate() {
        return getProperty("blog demo:date");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("blog demo:image", HippoGalleryImageSetBean.class);
    }
    
}
