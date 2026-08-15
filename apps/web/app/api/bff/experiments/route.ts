import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

/** List experiments for the tenant. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null }, { status: 400 });
  }
  try {
    const session = await authenticatedClient();
    const result = await session.client.listExperiments({}, { "X-Nexora-Organization-Id": organizationId });
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}

/** Create or update an experiment. */
export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as {
      organizationId?: string;
      experimentKey?: string;
      active?: boolean;
      treatmentPercentage?: number;
      description?: string;
    };
    if (!body.organizationId || !body.experimentKey) {
      return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId and experimentKey are required.", traceId: null }, { status: 400 });
    }
    const session = await authenticatedClient();
    await session.client.upsertExperiment(
      { experimentKey: body.experimentKey, active: body.active ?? false, treatmentPercentage: body.treatmentPercentage ?? 50, description: body.description },
      { "X-Nexora-Organization-Id": body.organizationId },
    );
    return session.applyCookies(NextResponse.json(null, { status: 200 }));
  } catch (error) {
    return problemResponse(error);
  }
}
