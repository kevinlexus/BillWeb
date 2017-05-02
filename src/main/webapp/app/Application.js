/**
 * The main application class. An instance of this class is created by app.js when it
 * calls Ext.application(). This is the ideal place to handle application launch and
 * initialization details.
 */
Ext.define('BillWebApp.Application', {
    extend: 'Ext.app.Application',
    
    name: 'BillWebApp',

    views: ['main.Main'],

    stores: {
    },
    
    launch: function () {
        // TODO - Launch the application
        console.log('Launch');
    },

    onAppUpdate: function () {
        Ext.Msg.confirm('Application Update', 'This application has an update, reload?',
            function (choice) {
                if (choice === 'yes') {
                    window.location.reload();
                }
            }
        );
    }
});
