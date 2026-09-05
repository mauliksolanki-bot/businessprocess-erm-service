"use client";

import React from "react";
import { Ticket } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import TicketForm from "@/components/ui/ticket-form";

export default function RaiseTicketPage() {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-3">
            <Ticket className="h-5 w-5 text-blue-600" />
            Raise a Ticket
          </CardTitle>
        </CardHeader>
        <CardContent>
          <TicketForm />
        </CardContent>
      </Card>
    </div>
  );
}
