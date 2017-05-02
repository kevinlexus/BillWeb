/*
 * This file is generated and updated by Sencha Cmd. You can edit this file as
 * needed for your application, but these edits will have to be merged by
 * Sencha Cmd when upgrading.
 */

Ext.application({
    name: 'BillWebApp',

    extend: 'BillWebApp.Application',

    requires: [
        'BillWebApp.view.main.Main'
    ],
    launch: function () {
        // TODO - Launch the application
        console.log('Launch2');
//        var store = Ext.getStore('OrgStore');
//        console.log('Launch2='+store);
//        store.on('load', function() {
            BillWebApp.getApplication().setMainView('main.Main');
//        });
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