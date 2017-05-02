Ext.define('BillWebApp.view.main.Panel4Controller', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panel4controller',

    // Формирование платежки
    onGenClick: function () {
        var genDt = this.lookupReference('genDt');
        var isFinalValue = this.lookupReference('isFinalValue');
        var isEndMonthValue = this.lookupReference('isEndMonthValue');

        console.log("onGenClick="+Ext.Date.format(genDt.getValue(), 'd.m.Y'));
        Ext.Ajax.request({
            url: '/genPayord',
            success: function (response) {
                alert("Формирование выполнено!");
            },
            params :{
                genDt: Ext.Date.format(genDt.getValue(), 'd.m.Y'),
                isFinal: isFinalValue.getValue(),
                isEndMonth: isEndMonthValue.getValue()
            },
            method : 'POST'
        });

    }


});
