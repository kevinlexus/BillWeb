/**
 * Created by lev on 03.04.2017.
 */
Ext.define('BillWebApp.store.KoStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.kostore',
    storeId: 'KoStore',
    model: 'BillWebApp.model.Ko',
/*    config:{
        autoLoad: true,
        autoSync: true
    },*/
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : '/base/getKoAddrTpFlt',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    }, listeners: {
        load: function() {
            console.log("KoStore loaded!");
        }
    }
});