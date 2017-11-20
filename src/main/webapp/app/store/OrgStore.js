/**
 * Created by lev on 09.03.2017.
 */
/*var rrr = Ext.create('Ext.window.Window', {
    title: 'LOAD!',
    height: 100,
    width: 300,
    layout: 'fit',
    items: {
        xtype: 'panelgauge'
    }
});*/

Ext.define('BillWebApp.store.OrgStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.orgstore',
    storeId: 'OrgStore',
    model: 'BillWebApp.model.Org',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : '/base/getOrgAll',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    },
    filter: function(filters, value) {
        Ext.data.Store.prototype.filter.apply(this, [
            filters,
            value ? new RegExp(Ext.String.escapeRegex(value), 'i') : value
        ]);
    }
});