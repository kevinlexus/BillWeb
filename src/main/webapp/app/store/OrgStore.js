/**
 * Created by lev on 09.03.2017.
 */
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
    }, listeners: {
        load: function() {
            console.log("OrgStore loaded!");
        }
    },
    filter: function(filters, value) {
        Ext.data.Store.prototype.filter.apply(this, [
            filters,
            value ? new RegExp(Ext.String.escapeRegex(value), 'i') : value
        ]);
    }
});