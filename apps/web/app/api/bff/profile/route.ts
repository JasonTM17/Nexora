import { NextResponse, type NextRequest } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function GET() {
  try {
    const session = await authenticatedClient();
    const response = await session.client.getProfile();
    return session.applyCookies(NextResponse.json(response.data));
  } catch (error) {
    return problemResponse(error);
  }
}

export async function PUT(request: NextRequest) {
  const csrf = requireSameOrigin(request);
  if (csrf) return csrf;
  try {
    const body = await request.json();
    const session = await authenticatedClient();
    const response = await session.client.updateProfile(body);
    return session.applyCookies(NextResponse.json(response.data));
  } catch (error) {
    return problemResponse(error);
  }
}
