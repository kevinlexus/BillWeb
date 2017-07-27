/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.PayordGrpStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.payordgrpstore',
    model: 'BillWebApp.model.PayordGrp',
    autoLoad: true,
    autoSync: false,
    proxy: { // прокси должна находиться в модели. иначе ничего не будет работать при type: 'ajax'
        //autoSave: true,
        type: 'ajax',
        api: {
            create  : '/payord/addPayordGrp',
            read    : '/payord/getPayordGrpAll',
            update  : '/payord/setPayordGrp',
            destroy : '/payord/delPayordGrp'
        },
        reader: {
            type: 'json'
        },
        writer: {
            type: 'json',
            allowSingle: false, //запретить по одному отправлять отправлять объекты в Json - только массивом![объект] - иначе трудно описывать в Restful
            writeAllFields: true  //писать весь объект в json
        }
    }, listeners: {
        load: function() {
            console.log("PayordGrpStore loaded!");
        }
    }
});