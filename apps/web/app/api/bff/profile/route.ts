import { NextResponse, type NextRequest } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function GET() {
  try {
    const response = await (await authenticatedClient()).getProfile();
    return NextResponse.json(response.data, { headers: { "Cache-Control": "private, no-store" } });
  } catch (error) {
    return problemResponse(error);
  }
}

export async function PUT(request: NextRequest) {
  const csrf = requireSameOrigin(request);
  if (csrf) return csrf;
  try {
    const body = await request.json();
    const response = await (await authenticatedClient()).updateProfile(body);
    return NextResponse.json(response.data, { headers: { "Cache-Control": "private, no-store" } });
  } catch (error) {
    return problemResponse(error);
  }
}
