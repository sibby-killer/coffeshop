import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const paystackSecretKey = Deno.env.get("PAYSTACK_SECRET_KEY");
    if (!paystackSecretKey) {
      throw new Error("PAYSTACK_SECRET_KEY not configured");
    }

    const { amount, email, order_id, payment_method, phone, provider, callback_url } = await req.json();

    if (!amount || !email || !order_id) {
      return new Response(
        JSON.stringify({ error: "Missing required fields: amount, email, order_id" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const reference = `order_${order_id}_${Date.now()}`;

    // Build transaction payload
    const transactionBody: any = {
      amount: Math.round(amount),
      email,
      reference,
      callback_url: callback_url || "",
      metadata: {
        order_id,
        custom_fields: [
          {
            display_name: "Order ID",
            variable_name: "order_id",
            value: order_id,
          },
        ],
      },
    };

    // Add mobile money channel if selected
    if (payment_method === "mobile_money" && phone) {
      transactionBody.channel = "mobile_money";
      transactionBody.mobile_money = {
        phone: phone,
        provider: provider || "m-pesa",
      };
    }

    // Initialize Paystack transaction
    const response = await fetch("https://api.paystack.co/transaction/initialize", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${paystackSecretKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(transactionBody),
    });

    const data = await response.json();

    if (!data.status) {
      return new Response(
        JSON.stringify({ error: data.message || "Payment initialization failed" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Update order with payment reference
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (supabaseUrl && supabaseServiceKey) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);

      await supabase
        .from("orders")
        .update({
          payment_reference: data.data.reference,
          updated_at: new Date().toISOString(),
        })
        .eq("id", order_id);
    }

    return new Response(
      JSON.stringify({
        status: true,
        access_code: data.data.access_code,
        reference: data.data.reference,
        authorization_url: data.data.authorization_url || null,
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
