/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Ssl use Nexus Truststore combobox.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ssl.SslUseTrustStore', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-sslusetruststore',

  initComponent: function () {
    var me = this;

    if (!me.fieldLabel && !me.boxLabel) {
      me.fieldLabel = 'Use Nexus TrustStore';
    }
    if (me.fieldLabel === true) {
      me.fieldLabel = 'Use Nexus TrustStore';
    }
    if (me.boxLabel === true) {
      me.boxLabel = 'Use Nexus TrustStore';
    }

    me.items = {
      xtype: 'panel',
      layout: 'hbox',
      items: [
        {
          xtype: 'checkbox',
          name: me.name,
          itemId: me.name,
          boxLabel: me.boxLabel
        },
        {
          xtype: 'button',
          text: 'View Certificate',
          ui: 'plain',
          action: 'showcertificate',
          glyph: 'xf0a3@FontAwesome' /* fa-certificate */,
          margin: '0 0 0 5'
        }
      ]
    }

    me.callParent(arguments);
  }

});