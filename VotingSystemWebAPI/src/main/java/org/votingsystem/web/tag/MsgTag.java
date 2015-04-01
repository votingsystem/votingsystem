package org.votingsystem.web.tag;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgTag extends TagSupport {

    private static final Logger log = Logger.getLogger(MsgTag.class.getSimpleName());

    private String args;
    private String value;

    public void setArgs(String args) {
        this.args = args;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int doEndTag() throws JspException {
        String[] params = null;
        if(args != null) {
            params = args.split("#");
        }
        try {
            pageContext.getOut().println(MessageFormat.format(value, params));
        } catch (Exception e) {
            throw new JspException(e.toString());
        }
        return EVAL_PAGE;
    }
}