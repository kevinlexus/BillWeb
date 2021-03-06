/**
 * Created by lev on 09.03.2017.
 */
Ext.define('BillWebApp.store.ServStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.servstore',
    storeId: 'ServStore',
    model: 'BillWebApp.model.Serv',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : '/base/getServAll',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    }
});