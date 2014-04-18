/*
 * Copyright (c) 2008-2014 Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Health Check repository settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.healthcheck.HealthCheckRepositorySettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-healthcheck-repository-settings',

  config: {
    active: false,
    repository: undefined
  },

  items: [
    {
      xtype: 'nx-settingsform',
      paramOrder: ['repositoryId'],
      api: {
        load: 'NX.direct.healthcheck_RepositorySettings.read',
        submit: 'NX.direct.healthcheck_RepositorySettings.update'
      },
      settingsFormSuccessMessage: 'Health Check Repository Settings $action',
      editableCondition: NX.Conditions.isPermitted('nexus:healthcheck', 'update'),
      editableMarker: 'You do not have permission to update health check repository settings',

      items: [
        {
          xtype: 'hiddenfield',
          name: 'repositoryId'
        },
        {
          xtype: 'checkbox',
          name: 'eulaAccepted',
          hidden: true
        },
        {
          xtype: 'checkbox',
          name: 'enabled',
          fieldLabel: 'Enable',
          helpText: 'Enable analysis of this repository for security vulnerabilities and license issues.'
        }
      ]
    },
    {
      xtype: 'form',
      itemId: 'statusForm',
      title: 'Status',
      frame: true,
      hidden: true,

      bodyPadding: 10,
      margin: 10,

      items: {
        xtype: 'displayfield',
        name: 'status'
      }
    }
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.callParent(arguments);
    Ext.override(me.down('nx-settingsform'), {
      /**
       * @override
       * Block Ext.Direct load call if we do not have a repository id.
       */
      load: function () {
        var me = this;
        if (me.getForm().baseParams.repositoryId) {
          me.callParent(arguments);
        }
      },
      /**
       * @override
       * Block Ext.Direct submit call if EULA is not accepted & show EULA window.
       */
      submit: function () {
        var me = this;
        if (me.getForm().getFieldValues().eulaAccepted) {
          me.callParent(arguments);
        }
        else {
          Ext.widget('nx-coreui-healthcheck-eula');
        }
      }
    });

    Ext.override(me.down('nx-settingsform').getForm(), {
      /**
       * @override
       * Show status when settings form is updated.
       */
      setValues: function (values) {
        var statusForm = me.down('#statusForm');

        this.callParent(arguments);

        if (values && values.enabled && values.status) {
          statusForm.getForm().setValues(values);
          statusForm.show();
        }
        else {
          statusForm.hide();
        }
      }
    });
  },

  /**
   * @private
   * Preset form base params to repository id.
   */
  applyRepository: function (repositoryModel) {
    var me = this,
        form = me.down('nx-settingsform');

    form.getForm().baseParams = {
      repositoryId: repositoryModel ? repositoryModel.getId() : undefined
    };

    return repositoryModel;
  }

})
;
