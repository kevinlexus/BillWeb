/**
 * This class is the controller for the main view for the application. It is specified as
 * the "controller" of the Main view class.
 *
 * TODO - Replace this content of this view to suite the needs of your application.
 */
var askObjPanel;
Ext.define('BillWebApp.view.main.Panel1Controller', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panel1controller',

    // Проверка при включении чекбокса
    onChangePeriodType: function (chk , newValue , oldValue , eOpts) {
        //получить выбранные id
        var store = this.getViewModel().getStore('periodstore1');
        var period1 = this.lookupReference('period1');
        var period2 = this.lookupReference('period2');
        if (newValue) {
            period1.lastQuery = null;
            period1.setValue(null);
            period2.setValue(null);
            store.proxy.extraParams = { repCd: 'RptPayDocList', tp: 1};
            period2.hide();
        } else {
            period1.lastQuery = null;
            period1.setValue(null);
            period2.setValue(null);
            store.proxy.extraParams = {repCd: 'RptPayDocList',  tp: 0};
            period2.show();
        }
    },
    // Печать отчета по сверке платежек
    onCompareClick: function () {
        console.log('onPrintClick');
        var period1 = this.lookupReference('period1');
        var period2 = this.lookupReference('period2');
        var checkbox1 = this.lookupReference('checkbox1');
        //window.location.assign('/rep/payordFlow2?periodId1='+period1.getValue()+'&periodId2='+period2.getValue());
        window.open('/rep/payordFlow2?repCd=RptPayInfo3&periodId1='+period1.getValue()+'&periodId2='+period2.getValue(), '_blank');
    },
    // Печать отчета платежек
    onPrintClick: function () {
        console.log('onPrintClick');
        var period1 = this.lookupReference('period1');
        var period2 = this.lookupReference('period2');
        var checkbox1 = this.lookupReference('checkbox1');
        if (checkbox1.getValue()){
            //window.location.assign('/rep/payordFlow1?periodId='+period1.getValue());
            window.open('/rep/payordFlow1?repCd=RptPayInfo2&periodId='+period1.getValue(), '_blank');
        } else {
            //window.location.assign('/rep/payordFlow2?periodId1='+period1.getValue()+'&periodId2='+period2.getValue());
            window.open('/rep/payordFlow2?repCd=RptPayInfo2&periodId1='+period1.getValue()+'&periodId2='+period2.getValue(), '_blank');
        }
    }

});
