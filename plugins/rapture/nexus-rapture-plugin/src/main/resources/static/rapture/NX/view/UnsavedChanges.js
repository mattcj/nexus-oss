/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext*/

/**
 * Unsaved changes window.
 *
 * @since 3.0
 */
Ext.define('NX.view.UnsavedChanges', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-unsaved-changes',

  title: NX.I18n.get('GLOBAL_UNSAVED_TITLE'),
  defaultFocus: 'nx-discard',

  /**
   * @public
   * Panel with content to be saved
   */
  content: null,

  /**
   * @public
   * Function to call if content is to be discarded
   */
  callback: Ext.emptyFn,

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    Ext.apply(this, {
      items: {
        xtype: 'panel',
        ui: 'nx-inset',
        html: NX.I18n.get('GLOBAL_UNSAVED_MESSAGE'),
        buttonAlign: 'left',
        buttons: [
          {
            text: NX.I18n.get('GLOBAL_UNSAVED_DISCARD_BUTTON'),
            ui: 'nx-primary',
            itemId: 'nx-discard',
            handler: function () {
              // Discard changes and load new content
              me.content.resetUnsavedChangesFlag(true);
              me.callback();
              me.close();
            }
          },
          { text: NX.I18n.get('GLOBAL_UNSAVED_BACK_BUTTON'), handler: me.close, scope: me }
        ]
      }
    });

    me.callParent();
  }

});
