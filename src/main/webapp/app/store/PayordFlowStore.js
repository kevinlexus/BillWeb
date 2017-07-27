/**
 * Created by lev on 18.04.2017.
 */
Ext.define('BillWebApp.store.PayordFlowStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.payordflowstore',
    storeId: 'payordflowstore',
    model: 'BillWebApp.model.PayordFlow',
    config:{// перенести в модель, если нужно autoSync = false
        autoLoad: true,
        autoSync: false
    },
    proxy: {
        type: 'ajax',
        api: {
            create  : '',
            create  : '/payord/addPayordFlow',
            read    : '/payord/getPayordFlowByTpDt',
            update  : '/payord/setPayordFlow',
            destroy : '/payord/delPayordFlow'
        },
        reader: {
            type: 'json'
        },
        writer: {
            type: 'json',
            allowSingle: false, //запретить по одному отправлять отправлять объекты в Json - только массивом![объект] - иначе трудно описывать в Restful
            writeAllFields: true  //писать весь объект в json
        },
        extraParams : {
            tp : '2',
            dt: null
        }
    }, listeners: {
        load: function() {
            console.log("PayordFlowStore loaded!");
        }
    }

});