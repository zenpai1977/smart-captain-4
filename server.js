const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Initialize Supabase using environment variables set safely on Render
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
const supabase = createClient(supabaseUrl, supabaseKey);

// Endpoint called by Android app when driver submits UTR & rules
app.post('/api/activate', async (req, res) => {
    try {
        const { phone, plan, utr, minTrip, maxPickup, minFare, goTo } = req.body;
        
        const { data, error } = await supabase
            .from('drivers')
            .upsert([
                {
                    phone,
                    plan,
                    utr,
                    status: 'Pending',
                    min_trip: minTrip,
                    max_pickup: maxPickup,
                    min_fare: minFare,
                    go_to: goTo
                }
            ], { onConflict: ['phone'] });

        if (error) throw error;
        res.status(200).json({ success: true, message: 'Activation request submitted successfully' });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// Endpoint for your Web Admin Panel to fetch all driver requests live from Supabase
app.get('/api/admin/requests', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('drivers')
            .select('*')
            .order('phone', { ascending: false });

        if (error) throw error;
        res.status(200).json(data);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// Endpoint for your Web Admin Panel to approve a driver pass
app.post('/api/admin/approve', async (req, res) => {
    try {
        const { phone } = req.body;
        const { data, error } = await supabase
            .from('drivers')
            .update({ status: 'Active' })
            .eq('phone', phone);

        if (error) throw error;
        res.status(200).json({ success: true, message: `Driver ${phone} approved!` });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, error: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
