
package org.onehippo.forge.blog.componentsinfo;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;


public interface PageableListInfo extends GeneralListInfo {

    @Parameter(name = "pagesVisible",defaultValue="true", displayName = "Show pages", typeHint = ComponentWrapper.ParameterType.BOOLEAN)
    Boolean isPagesVisible();
}
