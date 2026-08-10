import { NextResponse } from "next/server";
import { authenticatedClient, problemResponse } from "../_shared";

export async function GET() {
  try {
    const response = await (await authenticatedClient()).getAccessContext();
    return NextResponse.json(response.data, { headers: { "Cache-Control": "private, no-store" } });
  } catch (error) {
    return problemResponse(error);
  }
}
