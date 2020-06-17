/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.ldap.LDAPForeignSecurityPrincipalAwareRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanel;

public class LDAPRoleServicePanel extends RoleServicePanel<LDAPForeignSecurityPrincipalAwareRoleServiceConfig> {

    private static final long serialVersionUID = 1L;

    class LDAPAuthenticationPanel extends FormComponentPanel {

        private static final long serialVersionUID = 1L;
        private final String usernameField;
        private final String passwordField;
        public LDAPAuthenticationPanel(String id, String usernameField, String passwordField) {
            super(id, new Model());
            this.usernameField = usernameField;
            this.passwordField = passwordField;
            add(new TextField(usernameField));
        
            PasswordTextField pwdField = new PasswordTextField(passwordField);
            // avoid reseting the password which results in an
            // empty password on saving a modified configuration
            pwdField.setResetPassword(false);
            add(pwdField);
        }

        public void resetModel() {
            get(usernameField).setDefaultModelObject(null);
            get(passwordField).setDefaultModelObject(null);
        }
    }

    class LDAPForeignAuthenticationPanel extends LDAPAuthenticationPanel {

        private static final long serialVersionUID = 1L;
        public LDAPForeignAuthenticationPanel(String id, String usernameField, String passwordField) {
            super(id, usernameField, passwordField);
        }
    }

    public LDAPRoleServicePanel(String id, IModel<LDAPForeignSecurityPrincipalAwareRoleServiceConfig> model) {
        super(id, model);
        add(new TextField("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        add(new TextField("groupSearchBase").setRequired(true));
        add(new TextField("groupSearchFilter"));
        add(new TextField("allGroupsSearchFilter"));
        add(new TextField("userFilter"));
        add(new AjaxCheckBox("bindBeforeGroupSearch") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                WebMarkupContainer c = (WebMarkupContainer) 
                        LDAPRoleServicePanel.this.get("authenticationPanelContainer");

                //reset any values that were set
                LDAPAuthenticationPanel ldapAuthenticationPanel = (LDAPAuthenticationPanel)c.get("authenticationPanel");
                ldapAuthenticationPanel.resetModel();
                ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                target.addComponent(c);
            }
        });

        LDAPAuthenticationPanel authPanel = new LDAPAuthenticationPanel("authenticationPanel", "user", "password");
        authPanel.setVisible(model.getObject().isBindBeforeGroupSearch());
        add(new WebMarkupContainer("authenticationPanelContainer")
            .add(authPanel).setOutputMarkupId(true));

        add(new TextField("foreignServerURL"));
        add(new CheckBox("foreignUseTLS"));
        add(new TextField("foreignDomainPrefix"));
        add(new TextField("foreignUserFilter"));
        add(new AjaxCheckBox("foreignBind") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                WebMarkupContainer c = (WebMarkupContainer) 
                        LDAPRoleServicePanel.this.get("foreignAuthenticationPanelContainer");

                //reset any values that were set
                LDAPAuthenticationPanel ldapAuthenticationPanel = (LDAPAuthenticationPanel)c.get("foreignAuthenticationPanel");
                ldapAuthenticationPanel.resetModel();
                ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                target.addComponent(c);
            }
        });

        LDAPForeignAuthenticationPanel foreignAuthPanel = new LDAPForeignAuthenticationPanel("foreignAuthenticationPanel", "foreignUser", "foreignPassword");
        boolean visible = model.getObject().isForeignBind() != null && model.getObject().isForeignBind().booleanValue();
        foreignAuthPanel.setVisible(visible);
        add(new WebMarkupContainer("foreignAuthenticationPanelContainer")
            .add(foreignAuthPanel).setOutputMarkupId(true));
    }
}
