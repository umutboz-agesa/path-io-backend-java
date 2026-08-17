-- Which GCL step's payload to use for template interpolation (null = last GCL step)
ALTER TABLE "insights" ADD COLUMN "gcl_data_step" integer;
