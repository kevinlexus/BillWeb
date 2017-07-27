/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.PayordStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.payordstore',
    model: 'BillWebApp.model.Payord',
    autoLoad: true,
    autoSync: false,
    proxy: { // прокси должна находиться в модели. иначе ничего не будет работать при type: 'ajax' - сомнительно! перенес обратно в store
        type: 'ajax',
        api: {
            create  : '/payord/addPayord',
            read    : '/payord/getPayord',
            update  : '/payord/setPayord',
            destroy : '/payord/delPayord'
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
            console.log("PayordStore loaded!");
        }
    }

});