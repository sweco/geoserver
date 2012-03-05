/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A XML editor based on CodeMirror
 * @author Andrea Aime 
 */
@SuppressWarnings("serial")
public class CodeMirrorEditor extends FormComponentPanel<String> {

    static final ResourceReference JS_REF = new ResourceReference(
        CodeMirrorEditor.class, "js/codemirror/codemirror.js");

    static final ResourceReference CSS_REF = new ResourceReference(
        CodeMirrorEditor.class, "js/codemirror/codemirror.css");

    static Map<String,Object> DEFAULT_OPTIONS = new HashMap<String, Object>();
    static {
        DEFAULT_OPTIONS.put("mode", "xml");
        DEFAULT_OPTIONS.put("lineNumbers", true);
    }

    private TextArea<String> editor;
    private WebMarkupContainer container;
    private Map<String,Object> options;
    private AbstractDefaultAjaxBehavior onBlurBehaviour;

    public CodeMirrorEditor(String id, IModel<String> model) {
        this(id, model, null);
    }
    
    public CodeMirrorEditor(String id, IModel<String> model, Map<String,Object> options) {
        super(id, model);
        
        container = new WebMarkupContainer("editorContainer");
        container.setOutputMarkupId(true);
        add(container);
        
        editor = new TextArea<String>("editor", new Model<String>((String) model.getObject()));
         
        container.add(editor);
        editor.setOutputMarkupId(true);

        this.options = new HashMap<String, Object>(DEFAULT_OPTIONS);
        if (options != null) {
            this.options.putAll(options);
        }
        editor.add(new CodeMirrorBehavior());

        if (handleOnBlur()) {
            onBlurBehaviour = new AbstractDefaultAjaxBehavior() {
                @Override
                protected void respond(AjaxRequestTarget target) {
                    onBlur(target);
                }
            };
            editor.add(onBlurBehaviour);
        }
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    protected void onBeforeRender() {
        editor.setModelObject(getModelObject());
        super.onBeforeRender();
    }

    @Override
    protected void convertInput() {
        editor.processInput();
        setConvertedInput(editor.getConvertedInput());
    }
    
    @Override
    public String getInput() {
        return editor.getInput();
    }
    
    public void setTextAreaMarkupId(String id) {
        editor.setMarkupId(id);
    }
    
    public String getTextAreaMarkupId() {
        return editor.getMarkupId();
    }
    
    public void reset() {
        super.validate();
        editor.validate();
        editor.clearInput();
    }

    public IAjaxCallDecorator getSaveDecorator() {
        // we need to force CodeMirror to update the textarea contents (which it hid)
        // before submitting the form, otherwise the validation will use the old contents
        return new AjaxCallDecorator() {
            @Override
            public CharSequence decorateScript(CharSequence script) {
                // textarea.value = codemirrorinstance.getCode()
                String id = getTextAreaMarkupId();
                return "document.getElementById('" + id + "').value = document.gsEditors." + id + ".getCode();" + script;
            }
        };
    }

    protected boolean handleOnBlur() {
        return false;
    }

    protected void onBlur(AjaxRequestTarget target) {
    }

    class CodeMirrorBehavior extends AbstractBehavior {

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.renderJavascriptReference(JS_REF);
            response.renderCSSReference(CSS_REF);

            String mode = (String) options.get("mode");
            response.renderJavascriptReference(new ResourceReference(CodeMirrorEditor.class, 
                String.format("js/codemirror/mode/%s/%s.js", mode, mode)));

            response.renderOnDomReadyJavascript(getInitJavascript());
        }

        private String getInitJavascript() {
            //build up the option object
            StringBuffer opts = new StringBuffer();

            for (Map.Entry<String, Object> e: options.entrySet()) {
                Object val = e.getValue(); 
                if (val == null) {
                    continue;
                }
                
                opts.append(e.getKey()).append(":");
                if (val instanceof String) {
                    opts.append("'").append(val).append("'");
                }
                else {
                    opts.append(val);
                }
                opts.append(",");
            }
            opts.setLength(opts.length() > 0 ? opts.length()-1 : opts.length());

            InputStream is = CodeMirrorEditor.class.getResourceAsStream("CodeMirrorEditor.js");
            String js = convertStreamToString(is);
            js = js.replaceAll("\\$options", opts.toString());
            js = js.replaceAll("\\$componentId", editor.getMarkupId());
            js = js.replaceAll("\\$container", container.getMarkupId());

            if (onBlurBehaviour != null) {
                js = js.replaceAll("\\$onBlurCallbackURL", "'" + onBlurBehaviour.getCallbackUrl() + "'");
            }
            else {
                js = js.replaceAll("\\$onBlurCallbackURL", "0");
            }

            return js;
        }

        public String convertStreamToString(InputStream is) {
            /*
             * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We
             * iterate until the Reader return -1 which means there's no more data to read. We use
             * the StringWriter class to produce the string.
             */
            try {
                if (is != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        is.close();
                    }
                    return writer.toString();
                } else {
                    return "";
                }
            } catch (IOException e) {
                throw new RuntimeException("Did not expect this one...", e);
            }
        }

    }

}
