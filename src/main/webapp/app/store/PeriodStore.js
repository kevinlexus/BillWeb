/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.PeriodStore1', {
    extend: 'Ext.data.Store',
    alias  : 'store.periodstore1',
    model: 'BillWebApp.model.Period',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : '/rep/getPeriodReports',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }/*,
        extraParams :{
            repCd : 'RptPayDocList',
            tp: '0'
        }*/
    }, listeners: {
        load: function() {
            console.log("PeriodStore1 loaded!");
        }
    }
});