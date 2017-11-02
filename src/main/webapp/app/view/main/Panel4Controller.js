Ext.define('BillWebApp.view.main.Panel4Controller', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panel4controller',

    // Формирование платежки
    onGenClick: function () {
        var genDt = this.lookupReference('genDt');
        var isFinalValue = this.lookupReference('isFinalValue');
        var isEndMonthValue = this.lookupReference('isEndMonthValue');

        console.log("onGenClick="+Ext.Date.format(genDt.getValue(), 'd.m.Y'));

        var window = Ext.create('Ext.window.Window', {
            //title: 'Сообщение',
            height: 100,
            width: 300,
            layout: 'fit',
            items: {
                xtype: 'panelgauge'
            }
        }).show();


        Ext.Ajax.request({
            url: '/genPayord',
            success: function (response, opts) {
                window.close();
                alert("Формирование выполнено!");
            },
            failure: function(response, opts) {
                window.close();
                alert("Ошибка во время формирования!");
            },
            params :{
                genDt: Ext.Date.format(genDt.getValue(), 'd.m.Y'),
                isFinal: isFinalValue.getValue(),
                isEndMonth: isEndMonthValue.getValue()
            },
            method : 'POST',
            timeout: 3600000 // 1час
        });

    }


});
