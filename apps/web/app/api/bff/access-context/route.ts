import { NextResponse } from "next/server";
import { authenticatedClient, problemResponse } from "../_shared";

export async function GET() {
  try {
    const session = await authenticatedClient();
    const response = await session.client.getAccessContext();
    return session.applyCookies(NextResponse.json(response.data));
  } catch (error) {
    return problemResponse(error);
  }
}
