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
/*global Ext, NX*/

/**
 * Assets grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-component-asset-list',
  requires: [
    'NX.I18n'
  ],

  /**
   * Currently shown component model.
   */
  componentModel: undefined,

  store: 'Asset',

  allowDeselect: true,

  viewConfig: {
    emptyText: 'No assets found',
    deferEmptyText: false
  },

  columns: [
    {
      xtype: 'nx-iconcolumn',
      dataIndex: 'contentType',
      width: 36,
      iconVariant: 'x16',
      iconNamePrefix: 'asset-type-',
      iconName: function(value) {
        if (value && NX.getApplication().getIconController().findIcon('asset-type-' + value.replace('/', '-'), 'x16')) {
          return value.replace('/', '-');
        }
        return 'default';
      }
    },
    {header: NX.I18n.get('BROWSE_SEARCH_ASSETS_NAME_COLUMN'), dataIndex: 'name', flex: 2.5}
  ],

  features: [
    {
      ftype: 'grouping',
      groupHeaderTpl: '{columnName}: {name}'
    }
  ],

  setComponentModel: function(componentModel) {
    var me = this;

    me.componentModel = componentModel;
    me.fireEvent('updated', me, me.componentModel);
  }

});
