/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mypackage.pipeline;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.RowCoder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.sdk.schemas.JavaFieldSchema;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.DoFn.OutputReceiver;
import org.apache.beam.sdk.transforms.DoFn.ProcessElement;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.transforms.Create;

import java.time.Instant;
import java.util.List;

// import javax.xml.validation.Schema;

import java.util.ArrayList;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;

/**
 * The {@link SamplePipeline} is a sample pipeline which can be used as a base for creating a real
 * Dataflow pipeline.
 *
 * <p><b>Pipeline Requirements</b>
 *
 * <ul>
 *   <li>Requirement #1
 *   <li>Requirement #2
 * </ul>
 *
 * <p><b>Example Usage</b>
 *
 * <pre>
 * # Set the pipeline vars
 * PROJECT_ID=PROJECT_ID
 * PIPELINE_FOLDER=gs://${PROJECT_ID}/dataflow/pipelines/sample-pipeline
 *
 * # Set the runner
 * RUNNER=DataflowRunner
 *
 * # Build the template
 * mvn compile exec:java \
 * -Dexec.mainClass=com.mypackage.pipeline.SamplePipeline \
 * -Dexec.cleanupDaemonThreads=false \
 * -Dexec.args=" \
 * --project=${PROJECT_ID} \
 * --stagingLocation=${PIPELINE_FOLDER}/staging \
 * --tempLocation=${PIPELINE_FOLDER}/temp \
 * --runner=${RUNNER} \
 * ADDITIONAL PARAMETERS HERE"
 * </pre>
 */
public class test {

  /*
   * The logger to output status messages to.
   */
  private static final Logger LOG = LoggerFactory.getLogger(SamplePipeline.class);

  /**
   * The {@link Options} class provides the custom execution options passed by the executor at the
   * command-line.
   */
  public interface Options extends DataflowPipelineOptions{
  }

  /**
   * The main entry-point for pipeline execution. This method will start the pipeline but will not
   * wait for it's execution to finish. If blocking execution is required, use the {@link
   * SamplePipeline#run(Options)} method to start the pipeline and invoke
   * {@code result.waitUntilFinish()} on the {@link PipelineResult}.
   *
   * @param args The command-line args passed by the executor.
   */
  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).as(Options.class);

    run(options);
  }

  @VisibleForTesting
  /**
   * A class used for parsing JSON web server events
   */
  @DefaultSchema(JavaFieldSchema.class)
  public static class CommonLog {
    String user_id;
    String ip;
    float lat;
    float lng;
    String timestamp;
    String http_request;
    String user_agent;
    int http_response;
    @javax.annotation.Nullable String num_bytes;
  }

  /**
   * Runs the pipeline to completion with the specified options. This method does not wait until the
   * pipeline is finished before returning. Invoke {@code result.waitUntilFinish()} on the result
   * object to block until the pipeline is finished running if blocking programmatic execution is
   * required.
   *
   * @param options The execution options.
   * @return The pipeline result.
   */
  public static PipelineResult run(Options options) {

    // Create the pipeline
    Pipeline pipeline = Pipeline.create(options);
    options.setJobName("sample-pipeline-" + System.currentTimeMillis());

     String input = "gs://dhodun1/events.json";
     String output = "dhodun1:logs.logs";

    /*
     * Steps:
     *  1) Read something
     *  2) Transform something
     *  3) Write something
     */

    pipeline
      .apply("ReadFromGCS", TextIO.read().from(input))
      .apply("FormatToRow", ParDo.of(new DoFn<String, CommonLog>() {
        @ProcessElement
        public void processElement(@Element String json, OutputReceiver<CommonLog> r) {
          Gson gson = new Gson();
          CommonLog commonLog = gson.fromJson(json, CommonLog.class);
          r.output(commonLog);
        }
      }))
      .apply("WriteToBQ", BigQueryIO.<CommonLog>write()
                              .to(output)
                              .useBeamSchema()
                              .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
                              .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))
      ;
    LOG.info("Building pipeline...");

    return pipeline.run();
  }
}
