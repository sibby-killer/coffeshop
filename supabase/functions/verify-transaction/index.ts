import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // Get Paystack secret key from environment
    const paystackSecretKey = Deno.env.get("PAYSTACK_SECRET_KEY");
    if (!paystackSecretKey) {
      throw new Error("PAYSTACK_SECRET_KEY not configured");
    }

    // Get reference from query params
    const url = new URL(req.url);
    const reference = url.searchParams.get("reference");

    if (!reference) {
      return new Response(
        JSON.stringify({ error: "Missing reference parameter" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Verify transaction with Paystack
    const response = await fetch(
      `https://api.paystack.co/transaction/verify/${reference}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${paystackSecretKey}`,
          "Content-Type": "application/json",
        },
      }
    );

    const data = await response.json();

    if (!data.status) {
      return new Response(
        JSON.stringify({ error: data.message || "Verification failed" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const transaction = data.data;

    // Update order status in database
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (supabaseUrl && supabaseServiceKey) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);

      // Extract order_id from reference (format: order_{order_id}_{timestamp})
      const orderIdMatch = reference.match(/^order_(.+)_\d+$/);
      const orderId = orderIdMatch ? orderIdMatch[1] : null;

      if (orderId) {
        const newStatus =
          transaction.status === "success" ? "paid" : "cancelled";

        await supabase
          .from("orders")
          .update({
            status: newStatus,
            payment_method: transaction.channel || "",
            updated_at: new Date().toISOString(),
          })
          .eq("id", orderId);
      }
    }

    return new Response(
      JSON.stringify({
        status: true,
        data: {
          reference: transaction.reference,
          amount: transaction.amount / 100, // Convert from kobo/cents
          status: transaction.status,
          channel: transaction.channel,
          paid_at: transaction.paid_at,
          customer: transaction.customer,
        },
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
