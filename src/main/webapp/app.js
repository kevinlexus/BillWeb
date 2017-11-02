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
        title: 'Формирование',
            iconCls: 'fa-cog',
            items: [{
            xtype: 'panel4'
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


        var orgStore = Ext.getStore('OrgStore');
        var payordGprStore = Ext.getStore('PayordGrpStore');
        var window = Ext.create('Ext.window.Window', {
            //title: 'Сообщение',
            height: 100,
            width: 300,
            layout: 'fit',
            items: {
                xtype: 'panelgauge'
            }
        }).show();

        if (!orgStore.isLoaded()) {
            orgStore.on('load', function() {
                if (!payordGprStore.isLoaded()) {

                    payordGprStore.on('load', function() {
                        addPaneledit();
                    });

                } else {
                    addPaneledit();
                    window.close();
                    //msg.close();
                }
            });
        } else {
            addPaneledit();
            window.close();
            //msg.close();
        }

        console.log('doLayout!');

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