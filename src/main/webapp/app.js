/*
 * This file is generated and updated by Sencha Cmd. You can edit this file as
 * needed for your application, but these edits will have to be merged by
 * Sencha Cmd when upgrading.
 */

function addPaneledit() {
    var mainView =BillWebApp.getApplication().getMainView();
    mainView.add(
    [
    {
            title: 'Формирование',
            iconCls: 'fa-cog',
            items: [{
                xtype: 'panel4'
            }]
    },
    {
        title: 'Редактирование',
        iconCls: 'fa-edit',
        reference: 'panelEdit',
        xtype: 'panelEdit'
    },{
        title: 'Платежки',
            iconCls: 'fa-inbox',
            items: [{
            xtype: 'panel1'
        }]
    },
    {
        title: 'Настройки платежек',
            iconCls: 'fa-cog',
        items: [{
        xtype: 'panel5'
        }]
    },

    {
    title: 'Параметры',
        iconCls: 'fa-cog',
        items: [{
        xtype: 'panelPar'
        }]
    }
    ]
    );
    mainView.doLayout;
}

// статус добавления панели
var addPanel = 0;

Ext.application({
    name: 'BillWebApp',

    extend: 'BillWebApp.Application',
    requires: [
        'BillWebApp.view.main.Main',
        'BillWebApp.view.main.PanelGauge'

    ],
    launch: function () {
        console.log('Launch the application');
        BillWebApp.getApplication().setMainView('main.Main');

/*        var md = BillWebApp.getApplication().getMainView();
        console.log('Check1='+md);
        var ss = md.config;
        console.log('Check2='+ss);*/

        /*        var orgStore = Ext.getStore('OrgStore');
        var payordGprStore = Ext.getStore('PayordGrpStore');

        var window = Ext.create('Ext.window.Window', {
            //title: 'Сообщение',
            height: 100,
            width: 300,
            layout: 'fit',
            items: {
                xtype: 'panelgauge'
            }
        }).show();*/

      /*  if (!orgStore.isLoaded()) {
            console.log('Check1');
            orgStore.on('load', function() {
                console.log('Check3');

                if (addPanel == 0) {
                    addPanel = 1;
                    if (!payordGprStore.isLoaded()) {
                        console.log('Check4');

                        payordGprStore.on('load', function () {
                            addPaneledit();
                        });

                    } else {
                        console.log('Check5');

                        addPaneledit();
                        window.close();
                        //msg.close();
                    }
                }
            });
            orgStore.load();
        } else {
            console.log('Check2');

            addPaneledit();
            window.close();
            //msg.close();
        }*/

        //console.log('doLayout!');

    },
    // The name of the initial view to create. With the classic toolkit this class
    // will gain a "viewport" plugin if it does not extend Ext.Viewport. With the
    // modern toolkit, the main view will be added to the Viewport.
    //
   // mainView: 'BillWebApp.view.main.Main'
	
    //-------------------------------------------------------------------------
    // Most customizations should be made to BillWebApp.Application. If you need to
    // customize this file, doing so below this section reduces the likelihood
    // of merge conflicts when upgrading to new versions of Sencha Cmd.
    //-------------------------------------------------------------------------
});


/*
Ext.application({
    name: 'BillWebApp',
    extend: 'BillWebApp.Application',
    launch: function () {
        //MyApp.model.User.load(25, {
//        success: function (record) {
        // Do user stuff
        Ext.widget('main');  // Create viewport
//        }
        //  });
    }
});

*/